import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('Checkboxes Page', () => {
	test.beforeEach(async ({ checkBoxesPage }) => {
		await checkBoxesPage.goto();
	});

	test('should display correct heading', async ({ checkBoxesPage }) => {
		await expect(checkBoxesPage.heading).toHaveText('Checkboxes');
	});

	test('should have 2 checkboxes and verify their default states @smoke', async ({
		checkBoxesPage,
	}) => {
		await expect(checkBoxesPage.allCheckboxes).toHaveCount(2);

		await expect(checkBoxesPage.checkbox1).not.toBeChecked();
		await expect(checkBoxesPage.checkbox2).toBeChecked();
	});

	test('should check/uncheck all the boxes', async ({ checkBoxesPage }) => {
		await checkBoxesPage.checkAll();
		await expect(checkBoxesPage.checkbox1).toBeChecked();
		await expect(checkBoxesPage.checkbox2).toBeChecked();

		await checkBoxesPage.uncheckAll();
		await expect(checkBoxesPage.checkbox1).not.toBeChecked();
		await expect(checkBoxesPage.checkbox2).not.toBeChecked();
	});

	test('should toggle checkbox state', async ({ checkBoxesPage }) => {
		await checkBoxesPage.toggleCheckBox(0);
		await expect(checkBoxesPage.checkbox1).toBeChecked();

		await checkBoxesPage.toggleCheckBox(0);
		await expect(checkBoxesPage.checkbox1).not.toBeChecked();
	});
});
