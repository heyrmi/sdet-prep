import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class MultipleWindowsPage extends BasePage {
	readonly heading: Locator;
	readonly newWindowLink: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h3');
		this.newWindowLink = page.locator('.example a');
	}

	async goto(): Promise<void> {
		await this.page.goto('/windows');
	}

	async clickOnNewWindow(): Promise<Page> {
		const newPagePromise = this.page.context().waitForEvent('page');
		await this.newWindowLink.click();
		const newPage = await newPagePromise;

		await newPage.waitForLoadState();

		return newPage;
	}

	async getNewWindowHeading(newPage: Page): Promise<string> {
		return (await newPage.locator('h3').textContent()) ?? '';
	}
}
