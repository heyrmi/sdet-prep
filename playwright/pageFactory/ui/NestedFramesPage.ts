import type { FrameLocator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class NestedFramesPage extends BasePage {
	readonly topFrame: FrameLocator;
	readonly bottomFrame: FrameLocator;
	readonly leftFrame: FrameLocator;
	readonly middleFrame: FrameLocator;
	readonly rightFrame: FrameLocator;

	constructor(page: Page) {
		super(page);

		// Top Level frames
		this.topFrame = page.frameLocator('frame[name="frame-top"]');
		this.bottomFrame = page.frameLocator('frame[name="frame-bottom"]');

		// Nested frames inside top frame
		this.leftFrame = this.topFrame.frameLocator('frame[name="frame-left"]');
		this.middleFrame = this.topFrame.frameLocator('frame[name="frame-middle"]');
		this.rightFrame = this.topFrame.frameLocator('frame[name="frame-right"]');
	}

	async goto(): Promise<void> {
		await this.page.goto('/nested_frames');
	}

	async getLeftFrameText(): Promise<string> {
		return (await this.leftFrame.locator('body').textContent()) ?? '';
	}

	async getMiddleFrameText(): Promise<string> {
		return (await this.middleFrame.locator('#content').textContent()) ?? '';
	}

	async getRightFrameText(): Promise<string> {
		return (await this.rightFrame.locator('body').textContent()) ?? '';
	}

	async getBottomFrameText(): Promise<string> {
		return (await this.bottomFrame.locator('body').textContent()) ?? '';
	}
}
