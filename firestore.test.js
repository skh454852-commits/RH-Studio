const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function validUserProfile(uid, ts) {
  return {
    userId: uid,
    username: `user_${uid}`,
    displayName: `Creator ${uid}`,
    bio: "Music & photo creator",
    followersCount: 0,
    followingCount: 0,
    postsCount: 0,
    verified: false,
    monetizationStatus: "not_applied",
    accountStatus: "active",
    privateProfile: false,
    allowDms: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated requests are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("withdrawal_requests").get());
  await assertFails(
    unauthDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorUsername: "alice",
      mediaType: "photo",
      caption: "Unauthorized",
      hashtags: [],
      copyrightConfirmed: true,
      likesCount: 0,
      commentsCount: 0,
      viewsCount: 0,
      status: "active",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile creation, shadow-field protection, and PII isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  // Valid create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, ts))
  );

  // Reject self-verification or pre-approved monetization on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserProfile(BOB_UID, ts),
      verified: true,
    })
  );

  // Reject shadow ghost field on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserProfile(BOB_UID, ts),
      isAdmin: true,
    })
  );

  // Private PII doc: owner can write & read, Bob cannot read
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("settings").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      payoutProvider: "upi",
      maskedPayoutDestination: "al***@okaxis",
      notifyLikes: true,
      notifyComments: true,
      notifyFollows: true,
      notifyEarnings: true,
      themeMode: "light",
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("settings").get()
  );
});

test("Posts, media chunks, likes, comments, and cross-user protections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  // Alice creates a photo post
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorUsername: "user_alice",
      mediaType: "music",
      title: "Sunset Lo-Fi",
      caption: "Chill beats #lofi",
      hashtags: ["lofi"],
      copyrightConfirmed: true,
      likesCount: 0,
      commentsCount: 0,
      viewsCount: 0,
      status: "active",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot edit Alice's caption or delete Alice's post
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      caption: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("posts").doc("post_1").delete());

  // Bob can like Alice's post and increment likesCount by 1
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID).set({
      userId: BOB_UID,
      createdAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot jump likesCount by +500
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 500,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob comments on Alice's post; Alice cannot edit Bob's comment
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      postId: "post_1",
      authorId: BOB_UID,
      authorName: "Bob",
      authorUsername: "user_bob",
      text: "Great track!",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("posts").doc("post_1").collection("comments").doc("c_1").update({
      text: "Edited by someone else",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Follow atomicity with existsAfter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set(validUserProfile(BOB_UID, ts))
  );

  // Updating followersCount without creating follower edge must fail
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      followersCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Atomic batch creating follower edge + incrementing followersCount succeeds
  const batch = aliceDb.batch();
  batch.set(
    aliceDb.collection("users").doc(BOB_UID).collection("followers").doc(ALICE_UID),
    { actorId: ALICE_UID, targetId: BOB_UID, actorName: "Alice", createdAt: ts }
  );
  batch.update(aliceDb.collection("users").doc(BOB_UID), {
    followersCount: 1,
    updatedAt: new Date(Date.now() - 1000),
  });
  await assertSucceeds(batch.commit());
});

test("Monetization, wallet withdrawals, query alignment, and admin RBAC + email spoofing test", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_1", {
      email: "parmanwithout@gmail.com",
      email_verified: false,
    })
    .firestore();
  const realAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "parmanwithout@gmail.com",
      email_verified: true,
    })
    .firestore();

  const ts = new Date(Date.now() - 5000);

  // Alice cannot initialize creator_earnings with fake balance
  await assertFails(
    aliceDb.collection("creator_earnings").doc(ALICE_UID).set({
      userId: ALICE_UID,
      eligibleViews: 0,
      estimatedRevenueUsd: 999.0,
      monthlyEarningsUsd: 999.0,
      availableBalanceUsd: 999.0,
      pendingBalanceUsd: 0,
      totalWithdrawnUsd: 0,
      monetizationStatus: "approved",
      updatedAt: ts,
    })
  );

  // Valid zeroed initialization succeeds
  await assertSucceeds(
    aliceDb.collection("creator_earnings").doc(ALICE_UID).set({
      userId: ALICE_UID,
      eligibleViews: 0,
      estimatedRevenueUsd: 0,
      monthlyEarningsUsd: 0,
      availableBalanceUsd: 0,
      pendingBalanceUsd: 0,
      totalWithdrawnUsd: 0,
      monetizationStatus: "not_applied",
      updatedAt: ts,
    })
  );

  // Spoofed unverified admin email MUST fail admin write
  await assertFails(
    spoofedAdminDb.collection("creator_earnings").doc(ALICE_UID).update({
      availableBalanceUsd: 100,
      monetizationStatus: "approved",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Verified admin can credit verified ad revenue and approve monetization
  await assertSucceeds(
    realAdminDb.collection("creator_earnings").doc(ALICE_UID).update({
      eligibleViews: 15000,
      estimatedRevenueUsd: 65.5,
      monthlyEarningsUsd: 65.5,
      availableBalanceUsd: 65.5,
      monetizationStatus: "approved",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice can submit withdrawal request and atomically move available -> pending
  await assertSucceeds(
    aliceDb.collection("withdrawal_requests").doc("w_1").set({
      userId: ALICE_UID,
      username: "user_alice",
      amountUsd: 30.0,
      payoutProvider: "upi",
      maskedDestination: "al***@okaxis",
      status: "pending",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("creator_earnings").doc(ALICE_UID).update({
      availableBalanceUsd: 35.5,
      pendingBalanceUsd: 30.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Filtered withdrawal query for owner succeeds; unfiltered non-admin query fails; Bob cannot read Alice's withdrawal
  await assertSucceeds(
    aliceDb.collection("withdrawal_requests").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("withdrawal_requests").get());
  await assertFails(bobDb.collection("withdrawal_requests").doc("w_1").get());
  await assertSucceeds(realAdminDb.collection("withdrawal_requests").get());
});

test("Direct messaging conversations enforce participant isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_999").firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").set({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      lastMessage: "Hey Bob!",
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").collection("messages").doc("m_1").set({
      conversationId: "conv_1",
      senderId: ALICE_UID,
      participantIds: [ALICE_UID, BOB_UID],
      text: "Hey Bob!",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.collection("conversations").where("participantIds", "array-contains", BOB_UID).get()
  );
  await assertFails(charlieDb.collection("conversations").doc("conv_1").get());
  await assertFails(
    charlieDb.collection("conversations").doc("conv_1").collection("messages").get()
  );
});
