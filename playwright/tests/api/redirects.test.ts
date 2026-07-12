import test from '@lib/ApiBaseTest';
import { expect } from '@playwright/test';
import { testConfig } from '@/testConfig';

test.describe('Redirects', () => {
	test('Auto-follows redirect chain', async ({ httpBin }) => {
		const response = await httpBin.get('/redirect/3');

		response.expectStatus(200);
		expect(response.url).toContain('/get');
	});

	test('Redirect to specific URL', async ({ httpBin }) => {
		const response = await httpBin.get('/redirect-to', {
			params: { url: `${testConfig.apiUrl}/get` },
		});

		response.expectStatus(200);
		expect(response.url).toContain('/get');
	});
});
