import {
	freeApiEnvelopeSchema,
	freeApiErrorSchema,
	loginResponseSchema,
	registerResponseSchema,
} from '@lib/api/schemas/freeapi.schemas';
import test from '@lib/FreeApiBaseTest';
import { expect } from '@playwright/test';

test.describe('Authentication', () => {
	const testPassword = 'TestPass123';

	test('should register a new user', async ({ freeApi }) => {
		const uniqueId = `auth_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
		const testEmail = `${uniqueId}@test.com`;
		const testUsername = uniqueId;

		const response = await freeApi.register(
			testEmail,
			testUsername,
			testPassword,
		);

		response.expectStatusIn([200, 201]);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(registerResponseSchema);
		schema.parse(body);

		if (body.success) {
			expect(body.data.user.email).toBe(testEmail);
			expect(body.data.user.username).toBe(testUsername);
			expect(body.data.user.role).toBe('USER');
		}
	});

	test('should reject registration with missing fields', async ({
		freeApi,
	}) => {
		const response = await freeApi.register('', '', '');

		response.expectStatus(422);
		const body = await response.json();

		const errorBody = freeApiErrorSchema.parse(body);
		expect(errorBody.success).toBe(false);
		expect(errorBody.errors.length).toBeGreaterThan(0);
	});

	test('should login with valid credentials', async ({ freeApi }) => {
		// Register first
		const regId = `login_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
		await freeApi.register(`${regId}@test.com`, regId, testPassword);

		const response = await freeApi.login(regId, testPassword);

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(loginResponseSchema);
		schema.parse(body);

		if (body.success) {
			expect(body.data.accessToken).toBeTruthy();
			expect(body.data.refreshToken).toBeTruthy();
			expect(body.data.user.username).toBe(regId);
		}
	});

	test('should reject login with wrong password', async ({ freeApi }) => {
		const regId = `wrongpw_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
		await freeApi.register(`${regId}@test.com`, regId, testPassword);

		const response = await freeApi.login(regId, 'WrongPassword999');

		response.expectStatusIn([401, 422]);
		const body = await response.json();
		expect(body.success).toBe(false);
	});

	test('should reject login with non-existent user', async ({ freeApi }) => {
		const response = await freeApi.login('nonexistent_user_12345', 'SomePass');

		response.expectStatusIn([401, 404, 422]);
		const body = await response.json();
		expect(body.success).toBe(false);
	});

	test('should access protected route with valid token', async ({
		authedFreeApi,
	}) => {
		const response = await authedFreeApi.getCurrentUser();

		response.expectStatus(200);
		const body = await response.json();

		expect(body.success).toBe(true);
		if (body.success) {
			expect(body.data.username).toBeTruthy();
			expect(body.data.email).toContain('@');
		}
	});

	test('should reject protected route without token', async ({ freeApi }) => {
		const response = await freeApi.getCurrentUser();

		response.expectStatus(401);
		const body = await response.json();

		const errorBody = freeApiErrorSchema.parse(body);
		expect(errorBody.message).toContain('Unauthorized');
	});
});
