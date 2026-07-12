import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class CheckboxesPage extends BasePage {
	readonly heading: Locator;
	readonly checkbox1: Locator;
	readonly checkbox2: Locator;
	readonly allCheckboxes: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h3');
		this.allCheckboxes = page.locator('.example #checkboxes input');
		this.checkbox1 = this.allCheckboxes.nth(0);
		this.checkbox2 = this.allCheckboxes.nth(1);
	}

	async goto(): Promise<void> {
		await this.page.goto('/checkboxes');
	}

	async checkAll(): Promise<void> {
		const count = await this.allCheckboxes.count();
		for (let i = 0; i < count; i++) {
			await this.allCheckboxes.nth(i).check();
		}
	}

	async uncheckAll(): Promise<void> {
		const count = await this.allCheckboxes.count();
		for (let i = 0; i < count; i++) {
			await this.allCheckboxes.nth(i).uncheck();
		}
	}

	async toggleCheckBox(index: number): Promise<void> {
		const cb = this.allCheckboxes.nth(index);

		if (await cb.isChecked()) {
			await cb.uncheck();
		} else {
			await cb.check();
		}
	}
}
