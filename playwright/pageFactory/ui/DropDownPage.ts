import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class DropDownPage extends BasePage {
	readonly heading: Locator;
	readonly dropdown: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h3');
		this.dropdown = page.locator('.example #dropdown');
	}

	async goto(): Promise<void> {
		await this.page.goto('/dropdown');
	}

	async selectOption(option: string): Promise<void> {
		await this.dropdown.selectOption(option);
	}
}
