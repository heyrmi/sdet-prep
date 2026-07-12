import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class AddRemovePage extends BasePage {
	readonly heading: Locator;
	readonly addElementButton: Locator;
	readonly deleteButton: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('#content h3');
		this.addElementButton = page.getByRole('button', { name: 'Add Element' });
		this.deleteButton = page.getByRole('button', { name: 'Delete' });
	}

	async goto(): Promise<void> {
		await this.page.goto('/add_remove_elements/');
	}

	async clickAddElement(times: number): Promise<void> {
		for (let i = 0; i < times; i++) {
			await this.addElementButton.click();
		}
	}

	async getDeleteButtonCount(): Promise<number> {
		return await this.deleteButton.count();
	}

	async clickDeleteButton(): Promise<void> {
		await this.deleteButton.first().click();
	}

	async clickAllDeleteButtons(): Promise<void> {
		const count = await this.getDeleteButtonCount();
		for (let i = 0; i < count; i++) {
			await this.deleteButton.first().click();
		}
	}
}
