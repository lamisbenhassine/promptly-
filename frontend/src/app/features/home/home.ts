import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';

import { Health } from '../../core/models/health.model';
import { HealthService } from '../../core/services/health.service';

@Component({
  selector: 'app-home',
  imports: [DatePipe],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home implements OnInit {

  private readonly healthService = inject(HealthService);

  /** Derniere reponse recue du backend, ou null si aucune. */
  protected readonly health = signal<Health | null>(null);

  /** Message d'erreur affiche lorsque le backend est injoignable. */
  protected readonly errorMessage = signal<string | null>(null);

  /** Vrai pendant l'appel HTTP, pour desactiver le bouton. */
  protected readonly loading = signal(false);

  /** Modules prevus, affiches sur la page d'accueil. */
  protected readonly modules = [
    { title: 'Cours', description: 'Java, Spring Boot, Angular, algorithmique.', done: true },
    { title: 'Chapitres', description: 'Contenus structures et exemples de code.', done: true },
    { title: 'Evaluations', description: 'Quiz a choix multiples et scores.', done: true },
    { title: 'Forum', description: 'Entraide et discussions techniques.', done: true },
    { title: 'Reclamations', description: 'Suivi des demandes des etudiants.', done: true },
    { title: 'Assistant IA', description: 'Fonctionnalites a definir (Ollama ou API externe).', done: false }
  ];

  ngOnInit(): void {
    this.checkBackend();
  }

  /** Interroge GET /api/health et met a jour l'affichage. */
  protected checkBackend(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.healthService.getHealth().subscribe({
      next: (response) => {
        this.health.set(response);
        this.loading.set(false);
      },
      error: () => {
        this.health.set(null);
        this.errorMessage.set(
          'Backend injoignable. Verifiez que Spring Boot est demarre sur le port 8080.'
        );
        this.loading.set(false);
      }
    });
  }
}
