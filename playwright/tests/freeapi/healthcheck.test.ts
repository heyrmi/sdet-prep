import test from '@lib/FreeApiBaseTest';
import { expect } from '@playwright/test';

test.describe('Healthcheck', () => {
	test('should return healthy status', async ({ freeApi }) => {
		const response = await freeApi.healthcheck();

		response.expectStatus(200);
		const body = await response.json();
		expect(body.success).toBe(true);
		expect(body.data).toBe('OK');
		expect(body.message).toBe('Health check passed');
	});

	test('should return JSON content-type', async ({ freeApi }) => {
		const response = await freeApi.healthcheck();

		response.expectHeader('content-type', /application\/json/);
	});
});
