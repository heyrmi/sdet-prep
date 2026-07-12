import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('Dropdown Page Test', () => {
	test.beforeEach(async ({ dropDownPage }) => {
		await dropDownPage.goto();
	});

	test('should display the heading', async ({ dropDownPage }) => {
		await expect(dropDownPage.heading).toBeVisible();
		await expect(dropDownPage.heading).toHaveText('Dropdown List');
	});

	test('should be able to perform select operations', async ({
		dropDownPage,
	}) => {
		await expect(dropDownPage.dropdown).toBeVisible();

		await dropDownPage.dropdown.selectOption({ label: 'Option 1' });
		await expect(dropDownPage.dropdown).toHaveValue('1');

		await dropDownPage.dropdown.selectOption({ label: 'Option 2' });
		await expect(dropDownPage.dropdown).toHaveValue('2');
	});

	test('should have only 3 option including the placeholder', async ({
		dropDownPage,
	}) => {
		const count = dropDownPage.dropdown.locator('option');
		await expect(count).toHaveCount(3);
	});
});
