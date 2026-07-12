import test from '@lib/ApiBaseTest';
import { expect } from '@playwright/test';

const UUID_V4_REGEX =
	/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

test.describe('Dynamic Data', () => {
	test('UUID v4 format validation', async ({ httpBin }) => {
		const response = await httpBin.getUuid();

		response.expectStatus(200);
		const body = await response.json();
		expect(body.uuid).toMatch(UUID_V4_REGEX);
	});

	test('Delayed response with timeout', async ({ httpBin }) => {
		const start = Date.now();
		const response = await httpBin.get('/delay/1', { timeout: 10_000 });
		const elapsed = Date.now() - start;

		response.expectStatus(200);
		expect(elapsed).toBeGreaterThanOrEqual(1000);
	});
});
