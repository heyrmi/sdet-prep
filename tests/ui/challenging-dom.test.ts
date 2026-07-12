import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('Challenging DOM page testing', () => {
	test.beforeEach(async ({ challengingDomPage }) => {
		await challengingDomPage.goto();
	});

	test('buttons are clickable using text locators', async ({
		challengingDomPage,
	}) => {
		await challengingDomPage.clickButtonByText('qux');
		await expect(challengingDomPage.heading).toBeVisible();
		await expect(challengingDomPage.heading).toHaveText('Challenging DOM');
	});

	test('table has correct structure', async ({ challengingDomPage }) => {
		const headers = await challengingDomPage.getTableHeaders();
		expect(headers).toContain('Lorem');
		expect(headers).toContain('Ipsum');

		const rowCount = await challengingDomPage.tableRows.count();
		expect(rowCount).toBe(10);
	});

	test('can read a specific cell data', async ({ challengingDomPage }) => {
		const firstRowFirstCell = await challengingDomPage.getCellValue(0, 0);
		expect(firstRowFirstCell).toBe('Iuvaret0');
	});

	test('can click edit/delete on specific row', async ({
		challengingDomPage,
	}) => {
		await challengingDomPage.clickRowAction(2, 'edit');
		await expect(challengingDomPage.page).toHaveURL(/#edit$/);
	});

	test('canvas element exists', async ({ challengingDomPage }) => {
		await expect(challengingDomPage.canvas).toBeVisible();
		const box = await challengingDomPage.canvas.boundingBox();
		expect(box?.height).toBeGreaterThan(0);
		expect(box?.width).toBeGreaterThan(0);
	});
});
