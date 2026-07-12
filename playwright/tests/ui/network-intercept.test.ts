import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('Network Interception Examples', () => {
	test('should intercept and mock API response on the home page', async ({
		homePage,
	}) => {
		// Intercept requests to /status_codes and return a custom response
		await homePage.page.route('**/status_codes', (route) =>
			route.fulfill({
				status: 200,
				contentType: 'text/html',
				body: '<h3>Mocked Page</h3><p>This response was intercepted by Playwright.</p>',
			}),
		);

		await homePage.page.goto('/status_codes');

		await expect(homePage.page.locator('h3')).toHaveText('Mocked Page');
		await expect(homePage.page.locator('p')).toContainText('intercepted');
	});

	test('should abort image requests to simulate broken images', async ({
		homePage,
	}) => {
		// Block all image requests
		await homePage.page.route('**/*.{png,jpg,jpeg,gif}', (route) =>
			route.abort(),
		);

		await homePage.page.goto('/broken_images');

		// All images should fail to load
		const images = homePage.page.locator('.example img');
		const count = await images.count();
		expect(count).toBeGreaterThan(0);

		for (let i = 0; i < count; i++) {
			const naturalWidth = await images
				.nth(i)
				.evaluate((el: HTMLImageElement) => el.naturalWidth);
			expect(naturalWidth).toBe(0);
		}
	});

	test('should capture and verify network request details', async ({
		loginPage,
	}) => {
		const requests: { url: string; method: string }[] = [];

		loginPage.page.on('request', (request) => {
			if (request.url().includes('/authenticate')) {
				requests.push({
					url: request.url(),
					method: request.method(),
				});
			}
		});

		await loginPage.goto();
		await loginPage.login('tomsmith', 'SuperSecretPassword!');

		expect(requests.length).toBeGreaterThan(0);
		expect(requests[0].method).toBe('POST');
		expect(requests[0].url).toContain('/authenticate');
	});
});
