import type { FrameLocator, Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class IFramePage extends BasePage {
	// This page can't be automated since the component is paid and have reached its free limit
	readonly heading: Locator;
	readonly editorFrame: FrameLocator;
	readonly editorBody: Locator;
	readonly alertCloseButton: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('.example h3');
		this.editorFrame = page.frameLocator('#mce_0_ifr');
		this.editorBody = this.editorFrame.locator('#tinymce');
		this.alertCloseButton = page.locator("div[role='alert'] [type='button']");
	}

	async goto(): Promise<void> {
		await this.page.goto('/iframe');
	}

	async clearEditor(): Promise<void> {
		await this.editorBody.clear();
	}

	async typeInEditor(text: string): Promise<void> {
		await this.editorBody.fill(text);
	}

	async getEditorText(): Promise<string> {
		return (await this.editorBody.textContent()) ?? '';
	}

	async appendText(text: string): Promise<void> {
		await this.editorBody.press('End');
		await this.editorBody.pressSequentially(text);
	}

	async closeAlertButton(): Promise<void> {
		await this.alertCloseButton.click();
	}
}
