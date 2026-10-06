import { Component } from '@angular/core';
import { Home } from './pages/home/home';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [Home],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}