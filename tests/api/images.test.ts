import test from '@lib/ApiBaseTest';
import { expect } from '@playwright/test';

test.describe('Images & Binary', () => {
	test('PNG magic bytes via .body()', async ({ httpBin }) => {
		const response = await httpBin.get('/image/png');

		response.expectStatus(200);
		response.expectHeader('content-type', 'image/png');
		const buffer = await response.body();
		// PNG magic bytes: 0x89 0x50 0x4E 0x47
		expect(buffer[0]).toBe(0x89);
		expect(buffer[1]).toBe(0x50);
		expect(buffer[2]).toBe(0x4e);
		expect(buffer[3]).toBe(0x47);
	});

	test('HEAD request for image headers only', async ({ httpBin }) => {
		const response = await httpBin.head('/image/jpeg');

		response.expectStatus(200);
		response.expectHeader('content-type', 'image/jpeg');
	});
});
