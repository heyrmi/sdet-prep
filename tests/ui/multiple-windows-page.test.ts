import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('Multiple Windows Page Test', () => {
	test.describe('main windows page', () => {
		test('should display correct heading', async ({ multipleWindowsPage }) => {
			await multipleWindowsPage.goto();
			await expect(multipleWindowsPage.heading).toHaveText(
				'Opening a new window',
			);
		});

		test('should open new window and verify content @smoke', async ({
			multipleWindowsPage,
		}) => {
			await multipleWindowsPage.goto();
			await expect(multipleWindowsPage.newWindowLink).toBeVisible();
			const newPage = await multipleWindowsPage.clickOnNewWindow();

			expect(newPage).toBeTruthy();
			expect(newPage.url()).toContain('/windows/new');

			expect(await multipleWindowsPage.getNewWindowHeading(newPage)).toContain(
				'New Window',
			);
		});
	});
});
