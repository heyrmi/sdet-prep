import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('Drag and Drop Page Tests', () => {
	test.beforeEach(async ({ dragAndDropPage }) => {
		await dragAndDropPage.goto();
	});

	test('should verify heading', async ({ dragAndDropPage }) => {
		await expect(dragAndDropPage.heading).toHaveText('Drag and Drop');
	});

	test('should swap elements when dragging A to B', async ({
		dragAndDropPage,
	}) => {
		//verify initial state
		expect(await dragAndDropPage.getColumnAText()).toBe('A');
		expect(await dragAndDropPage.getColumnBText()).toBe('B');

		// perform drag and drop
		await dragAndDropPage.dragAndDropAtoB();

		// Verify elements are swapped
		expect(await dragAndDropPage.getColumnAText()).toBe('B');
		expect(await dragAndDropPage.getColumnBText()).toBe('A');
	});

	test('should swap back when dragging B to A', async ({ dragAndDropPage }) => {
		// Swap once
		await dragAndDropPage.dragAndDropAtoB();
		// Swap back
		await dragAndDropPage.dragAndDropBtoA();

		// Verify back to original state
		expect(await dragAndDropPage.getColumnAText()).toBe('A');
		expect(await dragAndDropPage.getColumnBText()).toBe('B');
	});
});
