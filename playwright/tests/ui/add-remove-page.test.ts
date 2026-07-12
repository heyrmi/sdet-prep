import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('Add Remove Page Test', () => {
	test.beforeEach(async ({ addRemovePage }) => {
		await addRemovePage.goto();
	});

	test('verify add remove page heading', async ({ addRemovePage }) => {
		await expect(addRemovePage.heading).toContainText('Add/Remove Elements');
	});

	test('verify add and remove functionality @smoke', async ({
		addRemovePage,
	}) => {
		const times = 5;
		await addRemovePage.clickAddElement(times);
		expect(
			await addRemovePage.getDeleteButtonCount(),
			`Add Element button was not clicked ${times} times`,
		).toBe(times);

		await addRemovePage.clickDeleteButton();
		expect(
			await addRemovePage.getDeleteButtonCount(),
			'Delete/Remove button functionality is not working',
		).toBe(times - 1);

		await addRemovePage.clickAllDeleteButtons();
		expect(
			await addRemovePage.getDeleteButtonCount(),
			'All the delete buttons are not cleared',
		).toBe(0);
	});
});
