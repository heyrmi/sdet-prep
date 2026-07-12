import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class FramesPage extends BasePage {
	readonly heading: Locator;
	readonly nestedFramesLink: Locator;
	readonly iFrameLink: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h3');
		this.nestedFramesLink = page.locator("a[href='/nested_frames']");
		this.iFrameLink = page.locator("a[href='/iframe']");
	}

	async goto(): Promise<void> {
		await this.page.goto('/frames');
	}

	async goToNestedFrames(): Promise<void> {
		await this.nestedFramesLink.click();
	}

	async goToIFrames(): Promise<void> {
		await this.iFrameLink.click();
	}
}
