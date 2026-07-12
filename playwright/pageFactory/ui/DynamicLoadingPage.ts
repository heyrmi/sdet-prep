import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class DynamicLoadingPage extends BasePage {
	readonly heading: Locator;
	readonly subHeading: Locator;
	readonly startButton: Locator;
	readonly loading: Locator;
	readonly finishText: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h3');
		this.subHeading = page.locator('h3 ~ h4');
		this.startButton = page.locator('#start button');
		this.loading = page.locator('#loading');
		this.finishText = page.locator('#finish');
	}

	async goto(): Promise<void> {
		await this.page.goto('/dynamic_loading');
	}

	async gotoExample1(): Promise<void> {
		await this.page.goto('/dynamic_loading/1');
	}

	async gotoExample2(): Promise<void> {
		await this.page.goto('/dynamic_loading/2');
	}

	async clickStart(): Promise<void> {
		await this.startButton.click();
	}
}
