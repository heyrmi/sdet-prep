import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class BrokenImagePage extends BasePage {
	readonly heading: Locator;
	readonly images: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h3');
		this.images = page.locator('.example img');
	}

	async goto(): Promise<void> {
		await this.page.goto('/broken_images');
	}

	async getImageCount(type: 'broken' | 'valid'): Promise<number> {
		const images = await this.images.all();
		let count = 0;

		for (const img of images) {
			const naturalWidth = await img.evaluate(
				(el: HTMLImageElement) => el.naturalWidth,
			);

			const isBroken = naturalWidth === 0;

			if ((type === 'broken' && isBroken) || (type === 'valid' && !isBroken)) {
				count++;
			}
		}
		return count;
	}
}
