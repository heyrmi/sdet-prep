import test from '@lib/ApiBaseTest';
import { expect } from '@playwright/test';
import type { HttpBinAuthResponse } from '@/types/httpbin';

test.describe('Authentication', () => {
	test('Basic auth with valid credentials', async ({ authedHttpBin }) => {
		const response = await authedHttpBin.get<HttpBinAuthResponse>(
			'/basic-auth/admin/admin',
		);

		response.expectStatus(200);
		const body = await response.json();
		expect(body.authenticated).toBe(true);
		expect(body.user).toBe('admin');
	});

	test('Bearer token via custom header', async ({ httpBin }) => {
		const response = await httpBin.get<{ headers: Record<string, string> }>(
			'/headers',
			{ headers: { Authorization: 'Bearer test-token-123' } },
		);

		response.expectStatus(200);
		const body = await response.json();
		expect(body.headers.Authorization).toBe('Bearer test-token-123');
	});
});
