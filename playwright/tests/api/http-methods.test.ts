import test from '@lib/ApiBaseTest';
import { expect } from '@playwright/test';
import type { HttpBinGetResponse, HttpBinPostResponse } from '@/types/httpbin';

test.describe('HTTP Methods', () => {
	test('GET with query params', async ({ httpBin }) => {
		const response = await httpBin.get<HttpBinGetResponse>('/get', {
			params: { foo: 'bar', page: 2 },
		});

		response.expectStatus(200);
		const body = await response.json();
		expect(body.args.foo).toBe('bar');
		expect(body.args.page).toBe('2');
	});

	test('POST with JSON body', async ({ httpBin }) => {
		const payload = { name: 'playwright', version: 1 };
		const response = await httpBin.post<HttpBinPostResponse>('/post', {
			data: payload,
		});

		response.expectStatus(200);
		const body = await response.json();
		expect(body.json).toEqual(payload);
	});
});
