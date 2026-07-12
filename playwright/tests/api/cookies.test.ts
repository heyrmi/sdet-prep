import test from '@lib/ApiBaseTest';
import { expect } from '@playwright/test';
import type { HttpBinCookiesResponse } from '@/types/httpbin';

test.describe('Cookies', () => {
	test('Cookie persistence across requests', async ({ httpBin }) => {
		// Set a cookie via /cookies/set (auto-follows redirect to /cookies)
		await httpBin.get('/cookies/set', {
			params: { session: 'abc123' },
		});

		// Read cookies back — the context should retain them
		const response = await httpBin.get<HttpBinCookiesResponse>('/cookies');
		response.expectStatus(200);
		const body = await response.json();
		expect(body.cookies.session).toBe('abc123');
	});
});
