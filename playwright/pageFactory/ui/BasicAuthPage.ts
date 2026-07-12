import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class BasicAuthPage extends BasePage {
	readonly heading: Locator;
	readonly successMessage: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h3');
		this.successMessage = page.locator('.example p');
	}

	async goto(): Promise<void> {
		await this.page.goto('/basic_auth');
	}
}
