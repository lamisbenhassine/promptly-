import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Footer } from './shared/components/footer/footer';
import { Navbar } from './shared/components/navbar/navbar';

/**
 * Composant racine : il definit la mise en page commune
 * (barre de navigation, zone de contenu routee, pied de page).
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navbar, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
}
