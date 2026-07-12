import { freeApiErrorSchema } from '@lib/api/schemas/freeapi.schemas';
import test from '@lib/FreeApiBaseTest';
import { expect } from '@playwright/test';

test.describe('Error Handling', () => {
	test('should validate 401 error schema', async ({ freeApi }) => {
		const response = await freeApi.getCurrentUser();

		response.expectStatus(401);
		const body = await response.json();

		const errorBody = freeApiErrorSchema.parse(body);
		expect(errorBody.statusCode).toBe(401);
		expect(errorBody.data).toBeNull();
		expect(errorBody.success).toBe(false);
		expect(errorBody.message).toBeTruthy();
		expect(Array.isArray(errorBody.errors)).toBe(true);
	});

	test('should validate 404 error schema', async ({ freeApi }) => {
		const response = await freeApi.getUserById(99999);

		response.expectStatus(404);
		const body = await response.json();

		const errorBody = freeApiErrorSchema.parse(body);
		expect(errorBody.statusCode).toBe(404);
		expect(errorBody.data).toBeNull();
		expect(errorBody.success).toBe(false);
	});

	test('should validate 422 validation error schema with field errors', async ({
		freeApi,
	}) => {
		const response = await freeApi.register('', '', '');

		response.expectStatus(422);
		const body = await response.json();

		const errorBody = freeApiErrorSchema.parse(body);
		expect(errorBody.statusCode).toBe(422);
		expect(errorBody.errors.length).toBeGreaterThan(0);

		// Each error should be a record with string key/value
		for (const err of errorBody.errors) {
			const keys = Object.keys(err);
			expect(keys.length).toBeGreaterThan(0);
			for (const key of keys) {
				expect(typeof err[key]).toBe('string');
			}
		}
	});

	test('should always have non-empty message on errors', async ({
		freeApi,
	}) => {
		// 401 case
		const res401 = await freeApi.getCurrentUser();
		const body401 = await res401.json();
		expect(body401.message.length).toBeGreaterThan(0);

		// 404 case
		const res404 = await freeApi.getUserById(99999);
		const body404 = await res404.json();
		expect(body404.message.length).toBeGreaterThan(0);

		// 422 case
		const res422 = await freeApi.register('', '', '');
		const body422 = await res422.json();
		expect(body422.message.length).toBeGreaterThan(0);
	});
});
