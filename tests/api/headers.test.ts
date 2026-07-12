import test from '@lib/ApiBaseTest';
import { expect } from '@playwright/test';
import type { HttpBinHeadersResponse } from '@/types/httpbin';

test.describe('Headers', () => {
	test('Custom request header echoed in response body', async ({ httpBin }) => {
		const response = await httpBin.get<HttpBinHeadersResponse>('/headers', {
			headers: { 'X-Custom-Header': 'my-value' },
		});

		response.expectStatus(200);
		const body = await response.json();
		expect(body.headers['X-Custom-Header']).toBe('my-value');
	});

	test('Response headers via /response-headers', async ({ httpBin }) => {
		const response = await httpBin.get<Record<string, string>>(
			'/response-headers',
			{ params: { 'X-Test': 'hello' } },
		);

		response.expectStatus(200);
		response.expectHeader('x-test', 'hello');
	});
});
