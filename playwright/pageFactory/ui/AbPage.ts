import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class AbPage extends BasePage {
	readonly heading: Locator;
	readonly description: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('.example h3');
		this.description = page.locator('.example p');
	}

	async goto(): Promise<void> {
		await this.page.goto('/abtest');
	}
}
