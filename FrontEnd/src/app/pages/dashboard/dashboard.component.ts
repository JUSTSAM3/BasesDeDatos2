import { Component } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {

  resumen = [
    { titulo: 'Reservas hoy', valor: 12 },
    { titulo: 'Clientes activos', valor: 48 },
    { titulo: 'Espacios disponibles', valor: 6 },
    { titulo: 'Reservas pendientes', valor: 4 }
  ];

  reservas = [
    {
      cliente: 'Laura Gómez',
      espacio: 'Sala Ejecutiva',
      fecha: '08/10/2026',
      hora: '10:00',
      estado: 'Confirmada'
    },
    {
      cliente: 'Carlos Pérez',
      espacio: 'Auditorio',
      fecha: '09/10/2026',
      hora: '14:00',
      estado: 'Pendiente'
    },
    {
      cliente: 'Mariana Torres',
      espacio: 'Sala Creativa',
      fecha: '10/10/2026',
      hora: '09:00',
      estado: 'Confirmada'
    }
  ];
}