import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class FileUploadPage extends BasePage {
	readonly heading: Locator;
	readonly chooseFile: Locator;
	readonly upload: Locator;
	readonly uploadedFiles: Locator;
	readonly successMessage: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h3');
		this.chooseFile = page.locator('#file-upload');
		this.upload = page.locator('#file-submit');
		// this appear after successful upload
		this.uploadedFiles = page.locator('#uploaded-files');
		this.successMessage = page.locator('h3', { hasText: 'File Uploaded!' });
	}

	async goto(): Promise<void> {
		await this.page.goto('/upload');
	}

	async uploadFile(filePath: string): Promise<void> {
		await this.chooseFile.setInputFiles(filePath);
		await this.upload.click();
	}

	async uploadMultipleFiles(filePaths: string[]): Promise<void> {
		await this.chooseFile.setInputFiles(filePaths);
		await this.upload.click();
	}

	async getUploadedFileName(): Promise<string> {
		return (await this.uploadedFiles.textContent()) ?? '';
	}

	async clearFileSelection(): Promise<void> {
		await this.chooseFile.setInputFiles([]);
	}
}
