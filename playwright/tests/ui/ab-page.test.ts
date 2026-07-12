import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('AB Page Testing', () => {
	test.beforeEach(async ({ abPage }) => {
		await abPage.goto();
	});

	test('Verify title', async ({ abPage }) => {
		await expect(abPage.heading).toBeVisible();
		await expect(abPage.heading).toContainText('A/B Test');
	});

	test('Verify page paragraph', async ({ abPage }) => {
		await expect(abPage.description).toContainText(
			'Also known as split testing',
		);
	});
});
