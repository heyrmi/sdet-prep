import { AbPage } from '@pages/AbPage';
import { AddRemovePage } from '@pages/AddRemovePage';
import { BasicAuthPage } from '@pages/BasicAuthPage';
import { BrokenImagePage } from '@pages/BrokenImagePage';
import { ChallengingDomPage } from '@pages/ChallengingDomPage';
import { CheckboxesPage } from '@pages/CheckboxesPage';
import { ContextMenuPage } from '@pages/ContextMenuPage';
import { DataTablePage } from '@pages/DataTablePage';
import { DragAndDropPage } from '@pages/DragAndDropPage';
import { DropDownPage } from '@pages/DropDownPage';
import { DynamicLoadingPage } from '@pages/DynamicLoadingPage';
import { FileDownloaderPage } from '@pages/FileDownloaderPage';
import { FileUploadPage } from '@pages/FileUploadPage';
import { FramesPage } from '@pages/FramesPage';
import { HomePage } from '@pages/HomePage';
import { IFramePage } from '@pages/IFramePage';
import { JavaScriptAlertPage } from '@pages/JavaScriptAlertsPage';
import { LoginPage } from '@pages/LoginPage';
import { MultipleWindowsPage } from '@pages/MultipleWindowsPage';
import { NestedFramesPage } from '@pages/NestedFramesPage';
import { test as baseTest } from '@playwright/test';
import { testConfig } from '@/testConfig';

// UI fixtures
type UiFixtures = {
	homePage: HomePage;
	abPage: AbPage;
	addRemovePage: AddRemovePage;
	basicAuthPage: BasicAuthPage;
	brokenImagePage: BrokenImagePage;
	challengingDomPage: ChallengingDomPage;
	checkBoxesPage: CheckboxesPage;
	contextMenuPage: ContextMenuPage;
	dropDownPage: DropDownPage;
	dynamicLoadingPage: DynamicLoadingPage;
	loginPage: LoginPage;
	dragAndDropPage: DragAndDropPage;
	fileUploadPage: FileUploadPage;
	framesPage: FramesPage;
	nestedFramesPage: NestedFramesPage;
	iFramePage: IFramePage;
	multipleWindowsPage: MultipleWindowsPage;
	javaScriptAlertPage: JavaScriptAlertPage;
	dataTablePage: DataTablePage;
	fileDownloaderPage: FileDownloaderPage;
	secureFileDownloaderPage: FileDownloaderPage;
};

const test = baseTest.extend<UiFixtures>({
	homePage: async ({ page }, use): Promise<void> => {
		await use(new HomePage(page));
	},

	abPage: async ({ page }, use): Promise<void> => {
		await use(new AbPage(page));
	},

	addRemovePage: async ({ page }, use): Promise<void> => {
		await use(new AddRemovePage(page));
	},

	basicAuthPage: async ({ browser }, use): Promise<void> => {
		const context = await browser.newContext({
			httpCredentials: {
				username: testConfig.username,
				password: testConfig.password,
			},
		});

		const page = await context.newPage();
		await use(new BasicAuthPage(page));
		await context.close();
	},

	brokenImagePage: async ({ page }, use): Promise<void> => {
		await use(new BrokenImagePage(page));
	},

	challengingDomPage: async ({ page }, use): Promise<void> => {
		await use(new ChallengingDomPage(page));
	},

	checkBoxesPage: async ({ page }, use): Promise<void> => {
		await use(new CheckboxesPage(page));
	},

	contextMenuPage: async ({ page }, use): Promise<void> => {
		await use(new ContextMenuPage(page));
	},

	dropDownPage: async ({ page }, use): Promise<void> => {
		await use(new DropDownPage(page));
	},

	dynamicLoadingPage: async ({ page }, use): Promise<void> => {
		await use(new DynamicLoadingPage(page));
	},

	loginPage: async ({ page }, use): Promise<void> => {
		await use(new LoginPage(page));
	},

	dragAndDropPage: async ({ page }, use): Promise<void> => {
		await use(new DragAndDropPage(page));
	},

	fileUploadPage: async ({ page }, use): Promise<void> => {
		await use(new FileUploadPage(page));
	},

	framesPage: async ({ page }, use): Promise<void> => {
		await use(new FramesPage(page));
	},

	nestedFramesPage: async ({ page }, use): Promise<void> => {
		await use(new NestedFramesPage(page));
	},

	iFramePage: async ({ page }, use): Promise<void> => {
		await use(new IFramePage(page));
	},

	multipleWindowsPage: async ({ page }, use): Promise<void> => {
		await use(new MultipleWindowsPage(page));
	},

	javaScriptAlertPage: async ({ page }, use): Promise<void> => {
		await use(new JavaScriptAlertPage(page));
	},

	dataTablePage: async ({ page }, use): Promise<void> => {
		await use(new DataTablePage(page));
	},

	fileDownloaderPage: async ({ page }, use): Promise<void> => {
		await use(new FileDownloaderPage(page, '/download'));
	},

	secureFileDownloaderPage: async ({ browser }, use): Promise<void> => {
		const context = await browser.newContext({
			httpCredentials: {
				username: testConfig.username,
				password: testConfig.password,
			},
		});

		const page = await context.newPage();
		await use(new FileDownloaderPage(page, '/download_secure'));
		await context.close();
	},
});

export default test;
