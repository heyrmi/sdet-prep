import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class ContextMenuPage extends BasePage {
	readonly heading: Locator;
	readonly hotspot: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h3');
		this.hotspot = page.locator('#hot-spot');
	}

	async goto(): Promise<void> {
		await this.page.goto('/context_menu');
	}

	async rightClickHotSpot(): Promise<void> {
		await this.hotspot.click({ button: 'right' });
	}

	async getAlertText(): Promise<string> {
		return new Promise((resolve) => {
			this.page.once('dialog', async (dialog) => {
				resolve(dialog.message());
				await dialog.accept();
			});
		});
	}
}
