const test = require('node:test');
const fs = require('node:fs');
const path = require('node:path');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} = require('@firebase/rules-unit-testing');
const {
  doc,
  collection,
  getDoc,
  getDocs,
  query,
  where,
  limit,
  orderBy,
  serverTimestamp,
  setDoc,
  updateDoc,
  deleteDoc
} = require('firebase/firestore');

let env;
const validTour = {
  title: 'Kandy Heritage',
  duration: '2 Days',
  price: 120,
  categoryId: '',
  description: 'A sample tour',
  route: 'Colombo to Kandy',
  overview: 'Culture and scenery',
  imageUrl: '',
  active: false,
  featured: false
};
const validService = {
  type: 'Vehicle', title: 'Colombo Van', duration: 'Full day', price: 95,
  capacity: 7, pickup: 'Colombo Hotel', description: 'Vehicle listing',
  imageUrl: '', active: false
};

test.before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-smart-tourism',
    firestore: {
      host: '127.0.0.1',
      port: 8080,
      rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8')
    }
  });
});
test.beforeEach(async () => env.clearFirestore());
test.after(async () => { if (env) await env.cleanup(); });

async function seed(documentPath, data) {
  await env.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), documentPath), data);
  });
}

const client = (uid, email) => env.authenticatedContext(uid, { email }).firestore();

test('a user can create and edit only their own profile and wishlist', async () => {
  const alice = client('alice', 'alice@example.com');
  const bob = client('bob', 'bob@example.com');
  const profile = doc(alice, 'users/alice');
  await assertSucceeds(setDoc(profile, {
    uid: 'alice', email: 'alice@example.com', fname: 'Alice', lname: 'A',
    country: 'Sri Lanka', birthday: '01/01/2000', profilePic: ''
  }));
  await assertSucceeds(updateDoc(profile, { fname: 'Alicia' }));
  await assertFails(updateDoc(profile, { role: 'admin' }));
  await assertFails(setDoc(doc(bob, 'users/alice/wishlist/tour'), { title: 'Private' }));
  await assertSucceeds(setDoc(doc(alice, 'users/alice/wishlist/tour'), { title: 'Saved' }));
  await assertFails(getDoc(doc(bob, 'users/alice')));
  await assertFails(setDoc(doc(alice, 'users/alice'), {
    uid: 'alice', email: 'alice@example.com', role: 'admin'
  }));
});

test('only a trusted operator can grant the admin role', async () => {
  const alice = client('alice', 'alice@example.com');
  await assertFails(setDoc(doc(alice, 'admins/alice'), { active: true, role: 'admin' }));
  await seed('admins/alice', { active: true, role: 'admin' });
  await assertSucceeds(getDoc(doc(alice, 'admins/alice')));
  await assertFails(updateDoc(doc(alice, 'admins/alice'), { active: false }));
  await assertFails(getDoc(doc(client('bob', 'bob@example.com'), 'admins/alice')));
});

test('tourists can query only published tours; administrators can inspect drafts', async () => {
  await seed('admins/admin', { active: true, role: 'admin' });
  await seed('packages/draft', { ...validTour, active: false });
  await seed('packages/published', { ...validTour, active: true });
  const tourist = client('alice', 'alice@example.com');
  const admin = client('admin', 'admin@example.com');
  await assertSucceeds(getDocs(query(collection(tourist, 'packages'), where('active', '==', true))));
  await assertFails(getDocs(collection(tourist, 'packages')));
  await assertFails(getDoc(doc(tourist, 'packages/draft')));
  await assertSucceeds(getDocs(collection(admin, 'packages')));
});

test('only active admins can publish valid tours; nobody can delete them', async () => {
  await seed('admins/admin', { active: true, role: 'admin' });
  const admin = client('admin', 'admin@example.com');
  const tourist = client('alice', 'alice@example.com');
  await assertFails(setDoc(doc(tourist, 'packages/attempt'), validTour));
  await assertSucceeds(setDoc(doc(admin, 'packages/new'), validTour));
  await assertFails(updateDoc(doc(tourist, 'packages/new'), { active: true }));
  await assertFails(updateDoc(doc(admin, 'packages/new'), { price: -1 }));
  await assertFails(updateDoc(doc(admin, 'packages/new'), { adminOnly: true }));
  await assertSucceeds(updateDoc(doc(admin, 'packages/new'), { active: true, price: 125 }));
  await assertFails(deleteDoc(doc(admin, 'packages/new')));
  await seed('admins/admin', { active: false, role: 'admin' });
  await assertFails(updateDoc(doc(admin, 'packages/new'), { active: false }));
});

test('role names other than admin have no manager access', async () => {
  await seed('admins/provider', { active: true, role: 'provider' });
  const provider = client('provider', 'provider@example.com');
  await assertFails(getDocs(collection(provider, 'packages')));
  await assertFails(setDoc(doc(provider, 'packages/unauthorized'), validTour));
});

test('only published categories are visible to tourists', async () => {
  await seed('admins/admin', { active: true, role: 'admin' });
  await seed('categories/hidden', { title: 'Hidden', active: false });
  await seed('categories/culture', { title: 'Culture', active: true });
  const tourist = client('alice', 'alice@example.com');
  const admin = client('admin', 'admin@example.com');
  await assertSucceeds(getDocs(query(collection(tourist, 'categories'), where('active', '==', true))));
  await assertFails(getDoc(doc(tourist, 'categories/hidden')));
  await assertFails(setDoc(doc(tourist, 'categories/new'), { title: 'New', active: true }));
  await assertSucceeds(getDocs(collection(admin, 'categories')));
  await assertFails(setDoc(doc(tourist, 'categories/fake'), { title: 'Fake', active: true }));
  await assertSucceeds(setDoc(doc(admin, 'categories/draft'), {
    title: 'Nature', active: false, description: '', imageUrl: ''
  }));
  await assertFails(updateDoc(doc(admin, 'categories/draft'), { title: '' }));
  await assertFails(updateDoc(doc(admin, 'categories/draft'), { imageUrl: 'http://not-secure' }));
  await assertFails(updateDoc(doc(admin, 'categories/draft'), { unknownField: true }));
  await assertSucceeds(updateDoc(doc(admin, 'categories/draft'), { active: true }));
  await assertSucceeds(getDoc(doc(tourist, 'categories/draft')));
  await assertFails(deleteDoc(doc(admin, 'categories/draft')));
});

test('admin manages vehicles and guides; tourists read only published services', async () => {
  await seed('admins/admin', { active: true, role: 'admin' });
  const admin = client('admin', 'admin@example.com');
  const tourist = client('alice', 'alice@example.com');
  const anonymous = env.unauthenticatedContext().firestore();
  await assertFails(setDoc(doc(tourist, 'services/fake'), validService));
  await assertSucceeds(setDoc(doc(admin, 'services/van'), validService));
  await assertFails(getDoc(doc(tourist, 'services/van')));
  await assertFails(getDocs(collection(tourist, 'services')));
  await assertSucceeds(getDocs(query(collection(tourist, 'services'), where('active', '==', true))));
  await assertSucceeds(getDocs(collection(admin, 'services')));
  await assertFails(updateDoc(doc(admin, 'services/van'), { type: 'Tour' }));
  await assertFails(updateDoc(doc(admin, 'services/van'), { capacity: 0 }));
  await assertFails(updateDoc(doc(admin, 'services/van'), { price: -1 }));
  await assertFails(updateDoc(doc(admin, 'services/van'), { imageUrl: 'http://example.com' }));
  await assertFails(updateDoc(doc(admin, 'services/van'), { adminOnly: true }));
  await assertFails(updateDoc(doc(tourist, 'services/van'), { active: true }));
  await assertSucceeds(updateDoc(doc(admin, 'services/van'), { active: true }));
  await assertSucceeds(getDoc(doc(tourist, 'services/van')));
  await assertSucceeds(getDoc(doc(anonymous, 'services/van')));
  await assertFails(deleteDoc(doc(admin, 'services/van')));
  await assertSucceeds(setDoc(doc(admin, 'services/guide'), {
    ...validService, type: 'Guide', title: 'Local Guide', capacity: 8, active: true
  }));
  await seed('admins/admin', { active: false, role: 'admin' });
  await assertFails(updateDoc(doc(admin, 'services/guide'), { active: false }));
});

test('demo submissions are private, owned, and reviewable only by active admins', async () => {
  await seed('admins/admin', { active: true, role: 'admin' });
  const alice = client('alice', 'alice@example.com');
  const bob = client('bob', 'bob@example.com');
  const admin = client('admin', 'admin@example.com');
  const booking = {
    id: 'test-booking', uid: 'alice', email: 'alice@example.com', source: 'demo',
    title: 'Kandy Heritage', type: 'Tour', duration: '2 Days', price: '$120',
    route: 'Colombo to Kandy', pax: '2', pickup: 'Colombo', mobile: '0700000000',
    date: '2026-10-06', time: '10:00', paymentMethod: 'Card (simulation)',
    paymentStatus: 'Paid (simulation only)', reviewStatus: 'New',
    createdAt: serverTimestamp()
  };
  const path = 'demo_bookings/test-booking';
  await assertFails(setDoc(doc(bob, path), booking));
  await assertFails(setDoc(doc(alice, path), { ...booking, reviewStatus: 'Reviewed' }));
  await assertFails(setDoc(doc(alice, path), { ...booking, source: 'real' }));
  await assertFails(setDoc(doc(alice, path), { ...booking, paymentStatus: 'Paid' }));
  await assertSucceeds(setDoc(doc(alice, path), booking));
  await assertSucceeds(getDoc(doc(alice, path)));
  await assertFails(getDoc(doc(bob, path)));
  await assertFails(getDocs(collection(alice, 'demo_bookings')));
  await assertSucceeds(getDocs(query(collection(admin, 'demo_bookings'),
    orderBy('createdAt', 'desc'), limit(50))));
  await assertFails(getDocs(collection(admin, 'demo_bookings')));
  await assertFails(updateDoc(doc(alice, path), { reviewStatus: 'Reviewed' }));
  await assertFails(updateDoc(doc(admin, path), { uid: 'admin' }));
  await assertSucceeds(updateDoc(doc(admin, path), { reviewStatus: 'Reviewed' }));
  await assertFails(deleteDoc(doc(admin, path)));
  await assertFails(deleteDoc(doc(bob, path)));
  await seed('admins/admin', { active: false, role: 'admin' });
  await assertFails(getDocs(query(collection(admin, 'demo_bookings'), limit(50))));
  await assertSucceeds(deleteDoc(doc(alice, path)));
});

test('clients cannot forge or erase Stripe test payments and orders', async () => {
  await seed('admins/admin', { active: true, role: 'admin' });
  const alice = client('alice', 'alice@example.com');
  const bob = client('bob', 'bob@example.com');
  const admin = client('admin', 'admin@example.com');
  const path = 'demo_bookings/stripe-order';
  await seed(path, {
    id: 'stripe-order', uid: 'alice', email: 'alice@example.com', source: 'stripe_test',
    title: 'Kandy Heritage', type: 'Tour', price: '$120.00 USD (test)',
    paymentMethod: 'Stripe Checkout (test mode)', paymentStatus: 'Paid in test mode (no real money)',
    reviewStatus: 'New', createdAt: serverTimestamp()
  });
  await seed('stripe_sandbox_orders/stripe-order', { uid: 'alice', status: 'paid' });
  await assertSucceeds(getDoc(doc(alice, path)));
  await assertFails(getDoc(doc(bob, path)));
  await assertFails(deleteDoc(doc(alice, path)));
  await assertFails(updateDoc(doc(alice, path), { paymentStatus: 'Refunded' }));
  await assertSucceeds(updateDoc(doc(admin, path), { reviewStatus: 'Reviewed' }));
  await assertFails(getDoc(doc(alice, 'stripe_sandbox_orders/stripe-order')));
  await assertFails(setDoc(doc(alice, 'stripe_sandbox_orders/forged'), { status: 'paid' }));
});
