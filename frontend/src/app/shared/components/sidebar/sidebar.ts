import { Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

export interface SidebarLink {
  label: string;
  path: string;
  icon: string;
  enabled: boolean;
}

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class Sidebar {

  readonly links = input.required<SidebarLink[]>();
  readonly open = input(false);
  readonly showAdmin = input(false);
  readonly closed = output<void>();

  protected close(): void {
    this.closed.emit();
  }
}
