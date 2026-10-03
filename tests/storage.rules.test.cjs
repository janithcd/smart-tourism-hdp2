const test = require('node:test');
const fs = require('node:fs');
const path = require('node:path');
const { initializeTestEnvironment, assertFails, assertSucceeds } =
  require('@firebase/rules-unit-testing');
const { ref, uploadBytes, getMetadata, listAll, deleteObject } = require('firebase/storage');

let env;
test.before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-smart-tourism',
    storage: {
      host: '127.0.0.1',
      port: 9199,
      rules: fs.readFileSync(path.join(__dirname, '..', 'storage.rules'), 'utf8')
    }
  });
});
test.beforeEach(async () => env.clearStorage());
test.after(async () => { if (env) await env.cleanup(); });

test('only the signed-in owner can upload, read, replace, and delete an avatar', async () => {
  const alice = env.authenticatedContext('alice').storage();
  const bob = env.authenticatedContext('bob').storage();
  const guest = env.unauthenticatedContext().storage();
  const aliceAvatar = ref(alice, 'profile_photos/alice/avatar');
  const bobView = ref(bob, 'profile_photos/alice/avatar');
  const guestView = ref(guest, 'profile_photos/alice/avatar');
  const image = new Uint8Array([137, 80, 78, 71]);

  await assertFails(uploadBytes(bobView, image, { contentType: 'image/png' }));
  await assertFails(uploadBytes(guestView, image, { contentType: 'image/png' }));
  await assertSucceeds(uploadBytes(aliceAvatar, image, { contentType: 'image/png' }));
  await assertSucceeds(getMetadata(aliceAvatar));
  await assertFails(getMetadata(bobView));
  await assertFails(getMetadata(guestView));
  await assertFails(listAll(ref(alice, 'profile_photos/alice')));
  await assertSucceeds(uploadBytes(aliceAvatar, image, { contentType: 'image/jpeg' }));
  await assertFails(deleteObject(bobView));
  await assertSucceeds(deleteObject(aliceAvatar));
});

test('restricts file path, MIME type, empty files, and files over 5 MiB', async () => {
  const alice = env.authenticatedContext('alice').storage();
  const avatar = ref(alice, 'profile_photos/alice/avatar');
  await assertFails(uploadBytes(ref(alice, 'profile_photos/alice/other'),
    new Uint8Array([1]), { contentType: 'image/png' }));
  await assertFails(uploadBytes(ref(alice, 'packages/cover'),
    new Uint8Array([1]), { contentType: 'image/png' }));
  await assertFails(uploadBytes(avatar, new Uint8Array([1]), { contentType: 'text/plain' }));
  await assertFails(uploadBytes(avatar, new Uint8Array(), { contentType: 'image/png' }));
  await assertFails(uploadBytes(avatar, new Uint8Array(5 * 1024 * 1024 + 1),
    { contentType: 'image/png' }));
});
