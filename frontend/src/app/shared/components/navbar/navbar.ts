import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar {

  /**
   * Liens de navigation. Les modules non encore developpes sont marques
   * comme desactives : ils seront actives au fur et a mesure des phases.
   */
  protected readonly links = [
    { label: 'Accueil', path: '/', enabled: true },
    { label: 'Cours', path: '/cours', enabled: false },
    { label: 'Quiz', path: '/quiz', enabled: false },
    { label: 'Forum', path: '/forum', enabled: false },
    { label: 'Reclamations', path: '/reclamations', enabled: false }
  ];
}
