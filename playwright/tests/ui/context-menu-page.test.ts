import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('Context menu page', () => {
	test.beforeEach(async ({ contextMenuPage }) => {
		await contextMenuPage.goto();
	});

	test('should display correct heading', async ({ contextMenuPage }) => {
		await expect(contextMenuPage.heading).toHaveText('Context Menu');
	});

	test.fixme(
		'should trigger alert and dismiss it on right click',
		async ({ contextMenuPage }) => {
			await expect(contextMenuPage.hotspot).toBeVisible();

			await Promise.all([
				contextMenuPage.page.waitForEvent('dialog').then(async (dialog) => {
					expect(dialog.type()).toBe('alert');
					expect(dialog.message()).toBe('You selected a context menu');
					await dialog.dismiss();
				}),
				contextMenuPage.rightClickHotSpot(),
			]);
			await expect(contextMenuPage.hotspot).toBeVisible();
		},
	);

	test('should not trigger alert on regular left click', async ({
		contextMenuPage,
	}) => {
		let alertTriggered = false;
		contextMenuPage.page.on('dialog', () => {
			alertTriggered = true;
		});

		await contextMenuPage.hotspot.click();
		await contextMenuPage.page.waitForTimeout(500);
		expect(alertTriggered).toBe(false);
	});
});
