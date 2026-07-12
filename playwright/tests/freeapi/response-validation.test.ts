import test from '@lib/FreeApiBaseTest';
import { expect } from '@playwright/test';

test.describe('Response Validation', () => {
	test('all endpoints should return application/json content-type', async ({
		freeApi,
	}) => {
		const responses = await Promise.all([
			freeApi.healthcheck(),
			freeApi.getUsers({ limit: 1 }),
			freeApi.getProducts({ limit: 1 }),
			freeApi.getJokes({ limit: 1 }),
			freeApi.getBooks({ limit: 1 }),
			freeApi.getStocks({ limit: 1 }),
		]);

		for (const response of responses) {
			response.expectHeader('content-type', /application\/json/);
		}
	});

	test('all success responses should have success: true', async ({
		freeApi,
	}) => {
		const responses = await Promise.all([
			freeApi.healthcheck(),
			freeApi.getUsers({ limit: 1 }),
			freeApi.getUserById(1),
			freeApi.getRandomUser(),
			freeApi.getProducts({ limit: 1 }),
			freeApi.getRandomProduct(),
			freeApi.getRandomJoke(),
		]);

		for (const response of responses) {
			response.expectStatus(200);
			const body = await response.json();
			expect(body.success).toBe(true);
			expect(body.statusCode).toBe(200);
		}
	});

	test('all error responses should have success: false', async ({
		freeApi,
	}) => {
		const responses = await Promise.all([
			freeApi.getCurrentUser(), // 401
			freeApi.getUserById(99999), // 404
		]);

		for (const response of responses) {
			const body = await response.json();
			expect(body.success).toBe(false);
			expect(body.data).toBeNull();
		}
	});

	test('all success responses should have a non-empty message', async ({
		freeApi,
	}) => {
		const responses = await Promise.all([
			freeApi.healthcheck(),
			freeApi.getUsers({ limit: 1 }),
			freeApi.getRandomProduct(),
			freeApi.getRandomJoke(),
		]);

		for (const response of responses) {
			const body = await response.json();
			expect(body.message).toBeTruthy();
			expect(typeof body.message).toBe('string');
		}
	});

	test('response time should be under 5 seconds', async ({ freeApi }) => {
		const start = Date.now();
		const response = await freeApi.getUsers({ limit: 10 });
		const elapsed = Date.now() - start;

		response.expectStatus(200);
		expect(elapsed).toBeLessThan(5000);
	});
});
