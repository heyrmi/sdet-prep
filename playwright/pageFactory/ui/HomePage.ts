import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class HomePage extends BasePage {
	readonly heading: Locator;
	readonly totalLinks: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h1');
		this.totalLinks = page.locator('li a[href]');
	}

	async goto(): Promise<void> {
		await this.page.goto('/');
	}

	async getLinkCount(): Promise<number> {
		return await this.totalLinks.count();
	}
}
