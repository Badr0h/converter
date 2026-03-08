import { Routes } from '@angular/router';
import { AboutComponent } from './about/about.component';
import { PrivacyComponent } from './privacy/privacy.component';
import { TermsComponent } from './terms/terms.component';
import { ContactComponent } from './contact/contact.component';

export const PAGES_ROUTES: Routes = [
    { path: 'about', component: AboutComponent },
    { path: 'privacy', component: PrivacyComponent },
    { path: 'terms', component: TermsComponent },
    { path: 'contact', component: ContactComponent }
];
