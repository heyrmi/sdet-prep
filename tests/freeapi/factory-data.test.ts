import {
	freeApiEnvelopeSchema,
	registerResponseSchema,
	todoSchema,
} from '@lib/api/schemas/freeapi.schemas';
import {
	buildProduct,
	buildRegistration,
	buildTodo,
	seedFactories,
} from '@lib/data';
import test from '@lib/FreeApiBaseTest';
import { expect } from '@playwright/test';

/**
 * Demonstrates the test-data factory layer:
 *  - dynamic, unique registration data via `buildRegistration`
 *  - dynamic todo payloads via `buildTodo`
 *  - field overrides + deterministic seeding
 */
test.describe('Factory-generated test data', () => {
	test('registers a user with factory-built credentials', async ({
		freeApi,
	}) => {
		const user = buildRegistration();

		const response = await freeApi.register(
			user.email,
			user.username,
			user.password,
		);

		response.expectStatusIn([200, 201]);
		const body = await response.json();

		freeApiEnvelopeSchema(registerResponseSchema).parse(body);
		if (body.success) {
			expect(body.data.user.email).toBe(user.email);
			expect(body.data.user.username).toBe(user.username);
		}
	});

	test('creates a todo from a factory-built payload', async ({
		authedFreeApi,
	}) => {
		const todo = buildTodo({ title: 'Factory-driven todo' });

		const response = await authedFreeApi.createTodo(
			todo.title,
			todo.description,
		);

		response.expectStatusIn([200, 201]);
		const body = await response.json();

		freeApiEnvelopeSchema(todoSchema).parse(body);
		if (body.success) {
			expect(body.data.title).toBe('Factory-driven todo');
			expect(body.data.description).toBe(todo.description);
			expect(body.data.isComplete).toBe(false);
		}
	});

	test('field overrides win over generated defaults', async () => {
		const user = buildRegistration({ username: 'fixed_username' });
		expect(user.username).toBe('fixed_username');
		expect(user.email).toMatch(/@example\.com$/);

		const product = buildProduct({ price: 9.99, category: 'Books' });
		expect(product.price).toBe(9.99);
		expect(product.category).toBe('Books');
		expect(product.images.length).toBeGreaterThan(0);
	});

	test('seeding produces deterministic output', async () => {
		seedFactories(1234);
		const first = buildRegistration();
		seedFactories(1234);
		const second = buildRegistration();

		expect(first).toEqual(second);

		// Restore non-deterministic generation for other tests.
		seedFactories();
	});
});
