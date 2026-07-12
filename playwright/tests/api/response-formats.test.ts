import test from '@lib/ApiBaseTest';
import { expect } from '@playwright/test';

test.describe('Response Formats', () => {
	test('JSON response with content-type check', async ({ httpBin }) => {
		const response = await httpBin.get('/json');

		response.expectStatus(200);
		response.expectHeader('content-type', /application\/json/);
		const body = await response.json();
		expect(body).toBeDefined();
	});

	test('HTML response via .text()', async ({ httpBin }) => {
		const response = await httpBin.get('/html');

		response.expectStatus(200);
		response.expectHeader('content-type', /text\/html/);
		const html = await response.text();
		expect(html).toContain('<h1>');
	});
});
