const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_tm_101";
const BOB_UID = "bob_tm_202";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validListingPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    sellerName: "Mergen A.",
    title: "Toyota Camry XSE 2022",
    category: "cars",
    make: "Toyota",
    model: "Camry",
    year: 2022,
    priceTmt: 685000,
    mileageKm: 32000,
    transmission: "Awtomat",
    fuelType: "Benzin",
    engineVolume: "3.5 L",
    bodyType: "Sedan",
    color: "Ak",
    region: "Aşgabat",
    description: "Ýagdaýy gaty gowy, täze ýaly.",
    badge: "vip",
    status: "active",
    negotiable: true,
    tradeAvailable: false,
    creditAvailable: false,
    imageKey: "sedan",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("listings").where("status", "==", "active").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("inquiries").where("participantIds", "array-contains", ALICE_UID).get());
  await assertFails(unauth.collection("listings").doc("car_1").set(validListingPayload(ALICE_UID)));
});

test("User profile: owner can create/update/read, other users are denied (PII isolation)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Mergen",
      region: "Aşgabat",
      phone: "+993 65 123456",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      region: "Mary",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot read Alice's profile (contains phone PII)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ region: "Balkan", updatedAt: new Date(Date.now() - 1000) }));

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Listings: owner create/update/sold-lock, active query vs unfiltered query, cross-user protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Valid Kotlin create payload (NO @DocumentId `id` field)
  await assertSucceeds(
    aliceDb.collection("listings").doc("car_1").set(validListingPayload(ALICE_UID))
  );

  // Reject payload containing `id` field or future timestamp or ghost field
  await assertFails(
    aliceDb.collection("listings").doc("car_bad_id").set({
      ...validListingPayload(ALICE_UID),
      id: "car_bad_id",
    })
  );
  await assertFails(
    aliceDb.collection("listings").doc("car_future").set(
      validListingPayload(ALICE_UID, { createdAt: new Date(Date.now() + 120000) })
    )
  );
  await assertFails(
    aliceDb.collection("listings").doc("car_ghost").set({
      ...validListingPayload(ALICE_UID),
      verified: true,
    })
  );

  // Filtered active query succeeds for any signed-in user; unfiltered list fails
  await assertSucceeds(bobDb.collection("listings").where("status", "==", "active").get());
  await assertSucceeds(aliceDb.collection("listings").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("listings").get());

  // Bob cannot edit or delete Alice's listing
  await assertFails(
    bobDb.collection("listings").doc("car_1").update({
      priceTmt: 100,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("listings").doc("car_1").delete());

  // Owner partial update succeeds
  await assertSucceeds(
    aliceDb.collection("listings").doc("car_1").update({
      priceTmt: 670000,
      negotiable: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Mark as sold (terminal state) succeeds, then further updates are locked
  await assertSucceeds(
    aliceDb.collection("listings").doc("car_1").update({
      status: "sold",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("listings").doc("car_1").update({
      status: "active",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Sold listing is hidden from Bob's get, still readable by Alice
  await assertFails(bobDb.collection("listings").doc("car_1").get());
  await assertSucceeds(aliceDb.collection("listings").doc("car_1").get());
});

test("Favorites subcollection: strictly owner-isolated and unfiltered list works on empty/non-empty", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Empty collection list succeeds for owner
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("favorites").get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("car_1").set({
      listingId: "car_1",
      title: "Toyota Camry XSE 2022",
      category: "cars",
      priceTmt: 685000,
      year: 2022,
      mileageKm: 32000,
      region: "Aşgabat",
      imageKey: "sedan",
      badge: "vip",
      createdAt: now,
    })
  );

  // Bob cannot read or write Alice's favorites
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("favorites").doc("car_1").delete());

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("car_1").delete());
});

test("Inquiries: relational listing existence check, participant isolation, and seller reply", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_303").firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(aliceDb.collection("listings").doc("car_1").set(validListingPayload(ALICE_UID)));

  // Orphaned inquiry for non-existent listing must fail
  await assertFails(
    bobDb.collection("inquiries").doc("inq_orphan").set({
      listingId: "non_existent",
      listingTitle: "Ghost Car",
      senderId: BOB_UID,
      senderName: "Serdar",
      sellerId: ALICE_UID,
      sellerName: "Mergen A.",
      participantIds: [BOB_UID, ALICE_UID],
      message: "Baha düşýäňizmi?",
      offerPriceTmt: 650000,
      status: "new",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid inquiry from Bob to Alice
  await assertSucceeds(
    bobDb.collection("inquiries").doc("inq_1").set({
      listingId: "car_1",
      listingTitle: "Toyota Camry XSE 2022",
      senderId: BOB_UID,
      senderName: "Serdar",
      sellerId: ALICE_UID,
      sellerName: "Mergen A.",
      participantIds: [BOB_UID, ALICE_UID],
      message: "Salam, obmen bolýarmy?",
      offerPriceTmt: 650000,
      status: "new",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Participant queries succeed; outsider and unfiltered query fail
  await assertSucceeds(aliceDb.collection("inquiries").where("participantIds", "array-contains", ALICE_UID).get());
  await assertSucceeds(bobDb.collection("inquiries").where("participantIds", "array-contains", BOB_UID).get());
  await assertFails(charlieDb.collection("inquiries").doc("inq_1").get());
  await assertFails(charlieDb.collection("inquiries").where("participantIds", "array-contains", ALICE_UID).get());

  // Seller (Alice) can reply; buyer (Bob) cannot forge seller reply
  await assertFails(
    bobDb.collection("inquiries").doc("inq_1").update({
      status: "replied",
      replyMessage: "Fake reply",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("inquiries").doc("inq_1").update({
      status: "replied",
      replyMessage: "Salam, hawa obmen seredýäris.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
