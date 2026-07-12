import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('Basic Auth Page Testing', () => {
	test.beforeEach(async ({ basicAuthPage }) => {
		await basicAuthPage.goto();
	});

	test('validate basic auth @smoke', async ({ basicAuthPage }) => {
		await expect(basicAuthPage.heading).toBeVisible();
		await expect(basicAuthPage.heading).toHaveText('Basic Auth');
		await expect(basicAuthPage.successMessage).toContainText(
			'Congratulations!',
		);
	});
});
