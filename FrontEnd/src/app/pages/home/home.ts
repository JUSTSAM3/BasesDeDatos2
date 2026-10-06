import { Component } from '@angular/core';

@Component({
  selector: 'app-home',
  standalone: true,
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home {

  espacios = [
    {
      nombre: 'Salón Aura',
      tipo: 'Eventos sociales',
      capacidad: 120,
      precio: '$250.000',
      estilo: 'aura'
    },
    {
      nombre: 'Terraza Nómada',
      tipo: 'Eventos al aire libre',
      capacidad: 80,
      precio: '$190.000',
      estilo: 'nomada'
    },
    {
      nombre: 'Studio 74',
      tipo: 'Eventos privados',
      capacidad: 40,
      precio: '$130.000',
      estilo: 'studio'
    }
  ];

  servicios = [
    {
      numero: '01',
      nombre: 'Catering',
      descripcion: 'Opciones gastronómicas para acompañar cada tipo de evento.'
    },
    {
      numero: '02',
      nombre: 'Decoración',
      descripcion: 'Ambientación pensada para transformar completamente el espacio.'
    },
    {
      numero: '03',
      nombre: 'Sonido',
      descripcion: 'Equipos y soluciones para música, presentaciones y celebraciones.'
    },
    {
      numero: '04',
      nombre: 'Producción',
      descripcion: 'Apoyo para organizar los detalles técnicos de tu experiencia.'
    }
  ];

}