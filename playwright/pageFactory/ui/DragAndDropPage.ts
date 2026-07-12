import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class DragAndDropPage extends BasePage {
	readonly heading: Locator;
	readonly columnA: Locator;
	readonly columnB: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h3');
		this.columnA = page.locator('#column-a');
		this.columnB = page.locator('#column-b');
	}

	async goto(): Promise<void> {
		await this.page.goto('/drag_and_drop');
	}

	async dragAndDropAtoB(): Promise<void> {
		await this.columnA.dragTo(this.columnB);
	}

	async dragAndDropBtoA(): Promise<void> {
		await this.columnB.dragTo(this.columnA);
	}

	async getColumnAText(): Promise<string> {
		return (await this.columnA.locator('header').textContent()) ?? '';
	}

	async getColumnBText(): Promise<string> {
		return (await this.columnB.locator('header').textContent()) ?? '';
	}
}
